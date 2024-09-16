#!/usr/bin/env groovy

/* set basic job configurations */
properties([
        buildDiscarder(logRotator(numToKeepStr: '50', artifactDaysToKeepStr: '10')), 
        disableConcurrentBuilds(), 
        pipelineTriggers([snapshotDependencies()])
    ])

node('jdk17') {
    try{
        stage('Prepare'){        
            checkout scm
        }
        
        stage('Assemble'){
            mvn "-U -DskipTests clean install"
        }

        if(false){
        stage('Test'){
            try{
                mvn "verify"
            }finally{
                junit '**/target/*-reports/TEST-*.xml'
                jacoco execPattern: '**/target/jacoco*.exec'    
            }
        }

        stage('Static Analysis'){
            try{
                withSonarQubeEnv('Sonarqube') {
                    def model = readMavenPom(file: 'pom.xml')
                    withMaven(
                        jdk: 'jdk17',
                        maven: 'default', 
                        mavenSettingsConfig: '05894f91-85e1-4e6d-8eb5-a101d90c62e3',
                        options: [
                            openTasksPublisher(highPriorityTaskIdentifiers: 'FIXME', lowPriorityTaskIdentifiers: 'TODO', normalPriorityTaskIdentifiers: 'PENDING', pattern: '**/*.*',excludePattern: '**/target/**')
                        ]
                    ) {
                        sh "mvn -DskipTests \
                            -Dsonar.projectKey=${model.getGroupId()}:${model.getArtifactId()}:${BRANCH_NAME.replace('/',"-")} \
                            -Dsonar.projectName=\"${model.getName()} ($BRANCH_NAME)\" \
                            pmd:cpd pmd:pmd sonar:sonar"
                    }
                }
            }finally{
                recordIssues (enabledForFailure: true, 
                    tools: [
                        cpd(pattern: '**/target/cpd.xml'),
                        pmdParser(pattern: '**/target/pmd.xml')
                    ])
            }
        }

        stage("Quality Gate"){
            timeout(time: 15, unit: 'MINUTES') {
                def qg = waitForQualityGate()
                if (qg.status == 'ERROR') {
                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                }else if (qg.status == 'WARN') {
                    currentBuild.result = 'UNSTABLE'
                }
            }
        }
        }

        stage('OCI Image'){
            withMaven(
                jdk: 'jdk17',
                maven: 'default',
                mavenSettingsConfig: '05894f91-85e1-4e6d-8eb5-a101d90c62e3',
                options: [
                    openTasksPublisher(highPriorityTaskIdentifiers: 'FIXME', lowPriorityTaskIdentifiers: 'TODO', normalPriorityTaskIdentifiers: 'PENDING', pattern: '**/*.*',excludePattern: '**/target/**')
                ]
            ) {
                sh "mvn -DskipTests -Pproduction package"
            }
            stash(name: 'appjar', includes: 'target/vaadinui-exec.jar')

//             node('docker'){
//                 checkout scm
//                 unstash('appjar')
//                 docker.withRegistry('', 'sedstef@hub.docker.com') {
//                     def image = docker.build("sedstef/opensbpm-vaadinui:${env.BUILD_ID}")
//                     image.push("${env.BUILD_ID}")
//                     image.push("latest")
//                 }
//             }
        }

        stage('Deploy'){
            retry(3) {
                mvn "-DskipTests deploy"
            }
        }
        
        currentBuild.result = 'SUCCESS'
    }catch(ex){
        currentBuild.result = 'FAILED'
        throw ex
    }finally{
        stage("Inform"){
            recordIssues enabledForFailure: true, tools: [
                mavenConsole(),
                java(),
                javaDoc()
            ]        
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
}

def mvn(String goals){
    withEnv(["PATH+NODEJS_HOME=${tool(type: 'nodejs', name: 'NodeJS 14.x')}"]) {
        withMaven(
            jdk: 'jdk11',
            maven: 'default', 
            mavenSettingsConfig: '05894f91-85e1-4e6d-8eb5-a101d90c62e3',
            options: [
                openTasksPublisher(highPriorityTaskIdentifiers: 'FIXME', lowPriorityTaskIdentifiers: 'TODO', normalPriorityTaskIdentifiers: 'PENDING', pattern: '**/*.*',excludePattern: '**/target/**')
            ]
        ) {
            sh "mvn $goals"
        }
    }
}
