#!/usr/bin/env groovy

/**
 * Jenkins Pipeline
 */
properties([
        buildDiscarder(logRotator(numToKeepStr: '10')), 
        disableConcurrentBuilds(),
        [$class: 'BeforeJobSnapshotJobProperty'], 
        pipelineTriggers([snapshotDependencies()])
    ])

node{
    try{
        stage('Prepare'){        
            checkout scm
        }
        
        stage('Assemble & Test'){
            withMaven(
                jdk: 'JDK 1.8',
                maven: 'default', 
                mavenSettingsConfig: '05894f91-85e1-4e6d-8eb5-a101d90c62e3'
            ) {
                sh "mvn -U -DskipTests clean install"
            }
        }
        
        stage('Test'){
            try{
                withMaven(
                    jdk: 'JDK 1.8',
                    maven: 'default', 
                    mavenSettingsConfig: '05894f91-85e1-4e6d-8eb5-a101d90c62e3',
                    options: [junitPublisher(disabled:true),jacocoPublisher(disabled:true)]
                ) {
                    sh "mvn verify"
                }
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
                        jdk: 'JDK 1.8',
                        maven: 'default', 
                        mavenSettingsConfig: '05894f91-85e1-4e6d-8eb5-a101d90c62e3',
                        options: [
                            openTasksPublisher(highPriorityTaskIdentifiers: 'FIXME', lowPriorityTaskIdentifiers: 'TODO', normalPriorityTaskIdentifiers: 'PENDING', pattern: '**/*.*',excludePattern: '**/target/**')
                        ]
                    ) {
                        //'JDK 1.8' is need for sonarqube (Hostname not verified (no certificates))
                        sh "mvn -DskipTests \
                                -Dsonar.projectKey=${model.getGroupId()}:${model.getArtifactId()}:${BRANCH_NAME} \
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

        stage('Deploy'){
            retry(3) {
                withMaven(jdk: 'JDK 1.8',
                    maven: 'default', 
                    mavenSettingsConfig: '05894f91-85e1-4e6d-8eb5-a101d90c62e3') {
                    sh "mvn -DskipTests deploy"
                }    
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
