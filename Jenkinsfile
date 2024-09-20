#!/usr/bin/env groovy

/* set basic job configurations */
properties([
        buildDiscarder(logRotator(numToKeepStr: '50', artifactDaysToKeepStr: '10')), 
        disableConcurrentBuilds(), 
        pipelineTriggers([snapshotDependencies()])
    ])

node('jdk17'){
    try{
        stage('Prepare'){        
            checkout scm

            env.JDK_HOME = tool(type: 'jdk',name: 'jdk17')
            env.PATH="${env.JDK_HOME}/bin:${env.PATH}"

        }

//        parallel buildService: {
            stage('Build Services'){
                withMaven(
                    jdk: 'jdk17',
                    maven: 'default',
                    mavenSettingsConfig: '05894f91-85e1-4e6d-8eb5-a101d90c62e3'
                ) {
                    withSonarQubeEnv('Sonarqube') {
                        sh "mvn clean test verify sonar:sonar package"
                    }
                    stash(name: 'engine-jar', includes: 'engine/service/target/engine-service-exec.jar')
                    stash(name: 'vaadinui-jar', includes: 'vaadinui/target/vaadinui-exec.jar')
                }
                junit '**/target/*-reports/TEST-*.xml'
                recordCoverage(name: 'Coverage Service',
                    tools: [[pattern: '**/build/reports/jacoco/**/*.xml']]
                )
                waitForQualityGate (abortPipeline: false)

                node('docker'){
                    checkout scm

                    unstash('engine-jar')
                    dir('engine/service'){
                        docker.withRegistry('', 'sedstef@hub.docker.com') {
                            def image = docker.build("sedstef/opensbpm-engine:${env.BUILD_ID}")
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
            stage('Build Frontend'){
                dir('frontend'){
                    sh "npm install"
                    sh "CI=true npm test -- --reporters=default --reporters=jest-junit --coverage"

                    withSonarQubeEnv(credentialsId: '90714a4b-9950-4c03-a361-89096c37b554') {
                        env.SONAR_HOME = tool(type: 'hudson.plugins.sonar.SonarRunnerInstallation',name: 'Sonar 4.x')
                        env.PATH="${env.SONAR_HOME}/bin:${env.PATH}"
                        sh 'sonar-scanner'
                    }

                    junit '**/test-results/*.xml'
                    recordCoverage(name: 'Coverage Frontend',
                        tools: [[parser: 'COBERTURA', pattern: '**/coverage/cobertura-coverage.xml']]
                    )

                    waitForQualityGate (abortPipeline: false)

                }
                node('docker'){
                    checkout scm
                    dir('frontend'){
                        docker.withRegistry('', 'sedstef@hub.docker.com') {
                            def frontenImage = docker.build("sedstef/opensbpm-frontend:${env.BUILD_ID}")
                            frontenImage.push("${env.BUILD_ID}")
                            frontenImage.push("latest")
                        }
                    }
                }
            }
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
