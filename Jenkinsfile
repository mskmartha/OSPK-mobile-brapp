@Library('stratusjenkins-sharedlibrary') _

sourceCodePipeline([buildNode: 'pcf-build-node']) {

    addStage('Validate', 'any:any') {
        withEnv(["ANDROID_SDK_ROOT=/usr/lib/android-sdk/", "JAVA_HOME=/usr/lib/jvm/java-1.11.0-openjdk-amd64/"]) {
            withCredentials([
                    usernamePassword(credentialsId: 'ospk-acupick-android-key-alias', passwordVariable: '_KEY_ALIAS', usernameVariable: 'aliasUser'),
                    usernamePassword(credentialsId: 'ospk-acupick-andriod-keystore-password	', passwordVariable: '_KEYSTORE_PASSWORD', usernameVariable: 'keystorePassword'),
                    usernamePassword(credentialsId: 'ospk-acupick-andriod-key-password', passwordVariable: '_KEY_PASSWORD', usernameVariable: 'keyPassword'),
                    file(credentialsId: 'ospk-acupick-andriod-key', variable: '_KEYSTORE')
            ]) {
                sh './gradlew --no-daemon clean ktlintCheck testInternalReleaseUnitTest lintInternalRelease jacocoTestInternalReleaseUnitTestReport'
            }
        }
    }

    /*addStage('veracode-pipeline-scan','master:any') {
        stratusJenkinsVeracode.pipelineScan([
            files: ["build/libs/${ARTIFACT_NAME}-${ARTIFACT_REVISION}-original.jar"],
            veracodeProfileId: 'OSPK-mobile-brapp',
            failonError: true
        ])
    }*/

    addStage('Build', 'release:any,hotfix:any,develop:any') {
        withEnv(["ANDROID_SDK_ROOT=/usr/lib/android-sdk/", "JAVA_HOME=/usr/lib/jvm/java-1.11.0-openjdk-amd64/"]) {
            withCredentials([
                    usernamePassword(credentialsId: 'ospk-acupick-android-key-alias', passwordVariable: '_KEY_ALIAS', usernameVariable: 'aliasUser'),
                    usernamePassword(credentialsId: 'ospk-acupick-andriod-keystore-password	', passwordVariable: '_KEYSTORE_PASSWORD', usernameVariable: 'keystorePassword'),
                    usernamePassword(credentialsId: 'ospk-acupick-andriod-key-password', passwordVariable: '_KEY_PASSWORD', usernameVariable: 'keyPassword'),
                    file(credentialsId: 'ospk-acupick-andriod-key', variable: '_KEYSTORE')
            ]) {
                sh './gradlew --no-daemon assembleInternalRelease assembleProductionRelease assembleProductionEastRelease assembleProductionCanaryRelease'
            }
        }
    }
    
    addStage('veracode-platform-scan','release:any') {
        stratusJenkinsVeracode.platformScan([
            uploadIncludesPattern: "app/build/outputs/apk/internal/release/*.apk",
            veracodeProfileId: 'OSPK-mobile-brapp',
            failonError: false
        ])
    }

    addStage('artifactory push', 'release:any,develop:any,hotfix:any') {
        sh '''
        mv app/build/outputs/mapping/internalRelease/mapping.txt app/build/outputs/mapping/internalRelease/internal-mapping.txt
        mv app/build/outputs/mapping/productionRelease/mapping.txt app/build/outputs/mapping/productionRelease/production-mapping.txt
        mv app/build/outputs/mapping/productionEastRelease/mapping.txt app/build/outputs/mapping/productionEastRelease/production-east-mapping.txt
        mv app/build/outputs/mapping/productionCanaryRelease/mapping.txt app/build/outputs/mapping/productionCanaryRelease/production-canary-mapping.txt
        '''

        artifact.push "app/build/outputs/apk/internal/release/*.apk"
        artifact.push "app/build/outputs/apk/production/release/*.apk"
        artifact.push "app/build/outputs/apk/productionEast/release/*.apk"
        artifact.push "app/build/outputs/apk/productionCanary/release/*.apk"
        artifact.push "app/build/outputs/mapping/internalRelease/internal-mapping.txt"
        artifact.push "app/build/outputs/mapping/productionRelease/production-mapping.txt"
        artifact.push "app/build/outputs/mapping/productionEastRelease/production-east-mapping.txt"
        artifact.push "app/build/outputs/mapping/productionCanaryRelease/production-canary-mapping.txt"
    }

    addStage("tag build", "release:any,hotfix:any,develop:any") {
        withCredentials([
                usernamePassword(credentialsId: 'stratusjenkins-github-enterprise-token', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')
        ]) {
            sh "git tag -a ${env.BRANCH_NAME}/${env.BUILD_NUMBER} -m 'Auto tagged Jenkins build from Albertsons'"
            sh "git push https://${GIT_PASSWORD}@github.albertsons.com/albertsons/OSPK-mobile-brapp.git --tags"
        }
    }
}
