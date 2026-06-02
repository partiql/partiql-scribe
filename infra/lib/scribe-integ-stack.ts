import * as cdk from 'aws-cdk-lib';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as glue from 'aws-cdk-lib/aws-glue';
import * as athena from 'aws-cdk-lib/aws-athena';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

export class ScribeIntegStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const accountId = cdk.Stack.of(this).account;

    // S3 bucket for test data and Athena query results
    const dataBucket = new s3.Bucket(this, 'DataBucket', {
      bucketName: `scribe-integ-data-${accountId}`,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      autoDeleteObjects: true,
      lifecycleRules: [
        {
          prefix: 'athena-results/',
          expiration: cdk.Duration.days(7),
        },
      ],
    });

    // Glue database (acts as the "default" catalog for Athena/Trino)
    const glueDatabase = new glue.CfnDatabase(this, 'GlueDatabase', {
      catalogId: accountId,
      databaseInput: {
        name: 'default',
        description: 'Scribe integration test database',
      },
    });

    // Athena workgroup for Trino engine
    const trinoWorkgroup = new athena.CfnWorkGroup(this, 'TrinoWorkgroup', {
      name: 'scribe-integ-trino',
      description: 'Scribe integration tests - Trino engine',
      state: 'ENABLED',
      workGroupConfiguration: {
        resultConfiguration: {
          outputLocation: `s3://${dataBucket.bucketName}/athena-results/`,
        },
        engineVersion: {
          selectedEngineVersion: 'Athena engine version 3',
        },
        bytesScannedCutoffPerQuery: 1_000_000_000, // 1 GB safety limit
      },
    });

    // IAM role for Athena Spark sessions
    const sparkRole = new iam.Role(this, 'SparkExecutionRole', {
      roleName: 'scribe-integ-spark-execution',
      assumedBy: new iam.ServicePrincipal('athena.amazonaws.com'),
      inlinePolicies: {
        sparkAccess: new iam.PolicyDocument({
          statements: [
            new iam.PolicyStatement({
              actions: ['s3:GetObject', 's3:ListBucket', 's3:PutObject', 's3:DeleteObject'],
              resources: [dataBucket.bucketArn, `${dataBucket.bucketArn}/*`],
            }),
            new iam.PolicyStatement({
              actions: ['glue:GetDatabase', 'glue:GetTable', 'glue:GetTables', 'glue:GetPartitions'],
              resources: ['*'],
            }),
          ],
        }),
      },
    });

    // Athena workgroup for Spark engine (Phase 2)
    const sparkWorkgroup = new athena.CfnWorkGroup(this, 'SparkWorkgroup', {
      name: 'scribe-integ-spark',
      description: 'Scribe integration tests - Spark engine',
      state: 'ENABLED',
      workGroupConfiguration: {
        executionRole: sparkRole.roleArn,
        resultConfiguration: {
          outputLocation: `s3://${dataBucket.bucketName}/spark-results/`,
        },
        engineVersion: {
          selectedEngineVersion: 'PySpark engine version 3',
        },
      },
    });
    sparkWorkgroup.addDependency(sparkRole.node.defaultChild as cdk.CfnResource);

    // Outputs
    new cdk.CfnOutput(this, 'BucketName', {
      value: dataBucket.bucketName,
      description: 'S3 bucket for test data',
    });

    new cdk.CfnOutput(this, 'TrinoWorkgroupName', {
      value: trinoWorkgroup.name!,
      description: 'Athena Trino workgroup',
    });

    new cdk.CfnOutput(this, 'SparkWorkgroupName', {
      value: sparkWorkgroup.name!,
      description: 'Athena Spark workgroup',
    });

    new cdk.CfnOutput(this, 'GlueDatabaseName', {
      value: 'default',
      description: 'Glue database name',
    });
  }
}
