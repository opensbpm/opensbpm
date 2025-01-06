#!/usr/bin/env groovy

/* set basic job configurations */
properties([
        buildDiscarder(logRotator(numToKeepStr: '50', artifactDaysToKeepStr: '10')), 
        disableConcurrentBuilds(), 
        pipelineTriggers([snapshotDependencies()])
    ])

node('docker'){
    try{
        stage('Prepare'){        
            checkout scm

            env.JDK_HOME = tool(type: 'jdk',name: 'jdk21')
            env.PATH="${env.JDK_HOME}/bin:${env.PATH}"

        }

//        parallel buildService: {
            stage('Build Services'){
                withMaven(
                    jdk: 'jdk21',
                    maven: 'default',
                    mavenSettingsConfig: '05894f91-85e1-4e6d-8eb5-a101d90c62e3'
                ) {
                    withSonarQubeEnv('Sonarqube') {
                        sh "mvn clean test verify sonar:sonar package -Pproduction"
                    }

//                    stash(name: 'engine-jar', includes: 'engine/service/target/engine-service-exec.jar')
                    stash(name: 'e2e-jar', includes: 'engine/e2e/target/e2e-exec.jar')
                    stash(name: 'vaadinui-jar', includes: 'vaadinui/target/vaadinui-exec.jar')
                }
                //junit '**/target/*-reports/TEST-*.xml'
                recordCoverage(name: 'Coverage Service',
                    tools: [[pattern: '**/build/reports/jacoco/**/*.xml']]
                )
                //waitForQualityGate (abortPipeline: false)
            }

            stage('Deploy OCI-Images'){
                withMaven(
                    jdk: 'jdk21',
                    maven: 'default',
                    mavenSettingsConfig: '05894f91-85e1-4e6d-8eb5-a101d90c62e3'
                ) {
                    docker.withRegistry('', 'opensbpm@hub.docker.com') {
                        sh """
                            mvn -pl engine/service spring-boot:build-image
                            docker push docker.io/opensbpm/engine:latest
                        """
                    }
                }
            }

            stage('Build OCI Images'){
                node('docker'){
                    checkout scm

                    dir('keycloak'){
                        docker.withRegistry('', 'sedstef@hub.docker.com') {
                            def image = docker.build("sedstef/keycloak-init:${env.BUILD_ID}")
                            image.push("${env.BUILD_ID}")
                            image.push("latest")
                        }
                    }


//                     unstash('engine-jar')
//                     dir('engine/service'){
//                         docker.withRegistry('', 'opensbpm@hub.docker.com') {
//                             def image = docker.build("opensbpm/opensbpm-engine:${env.BUILD_ID}")
//                             image.push("${env.BUILD_ID}")
//                             image.push("latest")
//                         }
//                     }

                    unstash('e2e-jar')
                    dir('engine/e2e'){
                        docker.withRegistry('', 'sedstef@hub.docker.com') {
                            def image = docker.build("sedstef/opensbpm-e2e:${env.BUILD_ID}")
                            image.push("${env.BUILD_ID}")
                            image.push("latest")
                        }
                    }

                    unstash('vaadinui-jar')
                    dir('vaadinui'){
                        docker.withRegistry('', 'sedstef@hub.docker.com') {
                            def image = docker.build("sedstef/opensbpm-vaadinui:${env.BUILD_ID}")
                            image.push("${env.BUILD_ID}")
                            image.push("latest")
                        }
                    }
                }
            }
//            }, buildFrontend: {
            /*
            stage('Apply Kubernetes'){
                node('kubectl'){
                    checkout scm
                    withKubeConfig( credentialsId: 'opensbpm@hetzner', serverUrl: 'https://cloud.opensbpm.org:16443') {
                        sh """
                            kustomize edit set image sedstef/keycloak-init=sedstef/keycloak-init:$BUILD_ID
                            kustomize edit set image sedstef/opensbpm-engine=sedstef/opensbpm-engine:$BUILD_ID
                            kustomize edit set image sedstef/opensbpm-vaadinui=sedstef/opensbpm-vaadinui:$BUILD_ID
                            kubectl apply -k .
                        """
                    }
                }
            }
            */
//            }
//            failFast: false

        currentBuild.result = 'SUCCESS'
    }catch(ex){
        if(currentBuild.result == null){
            currentBuild.result = 'FAILED'
        }
        throw ex
    }finally{
        emailext (recipientProviders: [culprits()],
            subject: "OpenSBPM Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' ${currentBuild.result}",
            body: """
                <p>${currentBuild.result}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
                <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>\n\
            """,
            attachLog: true
        )
    }
}

def waitForSonarqube(){
    timeout(time: 15, unit: 'MINUTES') {
        def qg = waitForQualityGate()
        if (qg.status == 'ERROR') {
            error "Pipeline aborted due to quality gate failure: ${qg.status}"
        }else if (qg.status == 'WARN') {
            currentBuild.result = 'UNSTABLE'
        }
    }
}
