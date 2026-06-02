package org.partiql.scribe.integ.config

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region

data class Config(
    val profile: String = "scribe-dev",
    val region: Region = Region.US_WEST_2,
    val athena: AthenaConfig = AthenaConfig(),
    val redshift: RedshiftConfig = RedshiftConfig(),
    val s3: S3Config = S3Config(),
) {
    val credentialsProvider: ProfileCredentialsProvider by lazy {
        ProfileCredentialsProvider.create(profile)
    }
}

data class AthenaConfig(
    val trinoWorkgroup: String = "scribe-integ-trino",
    val sparkWorkgroup: String = "scribe-integ-spark",
    val database: String = "default",
    val outputLocation: String = "s3://scribe-integ-data-686408868768/athena-results/",
)

data class RedshiftConfig(
    val clusterIdentifier: String = "scribe-cluster",
    val database: String = "scribe-db",
    val dbUser: String = "admin",
)

data class S3Config(
    val bucket: String = "scribe-integ-data-686408868768",
    val tablePrefix: String = "tables/",
    val metadataPrefix: String = "metadata/",
)
