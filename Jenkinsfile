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
        
        stage('Assemble'){
            mvn '-DskipTests clean verify install'
        }
        
        stage('Unit Tests'){
            //run surefire tests only
            mvn '-Dskip.failsafe.tests test'
            //junit '**/target/surefire-reports/TEST-*.xml'
            //jacoco execPattern: '**/target/jacoco.exec'
        }
    
        stage('Integration Tests'){
            //run failsafe tests only
            mvn '-Dskip.surefire.tests verify'
            //junit '**/target/failsafe-reports/TEST-*.xml'
            //jacoco execPattern: '**/target/jacoco-it.exec'
        }
        
        timeout(time: 15, unit: 'MINUTES') {
            stage('Static Analysis'){
                withSonarQubeEnv('Sonarqube') {
                    def model = readMavenPom(file: 'pom.xml')
                    mvn "-DskipTests \
                        -Dsonar.projectKey=${model.getGroupId()}:${model.getArtifactId()}:${BRANCH_NAME} \
                        -Dsonar.projectName=\"${model.getName()} ($BRANCH_NAME)\" \
                        pmd:cpd pmd:pmd sonar:sonar"
                }
            }
        
            stage("Quality Gate"){
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
                    sh "mvn deploy"
                }    
            }
        }
        
        currentBuild.result = 'SUCCESS'
    }catch(ex){
        currentBuild.result = 'FAILED'
        throw ex
    }finally{
        stage("Collect Results"){
            junit '**/target/*-reports/TEST-*.xml'
            jacoco execPattern: '**/target/jacoco*.exec'
    
            recordIssues enabledForFailure: true, tools: [
                mavenConsole(),
                java(),
                javaDoc(),
                cpd(pattern: '**/target/cpd.xml'), 
                pmdParser(pattern: '**/target/pmd.xml'),
                taskScanner(highTags: 'FIXME', lowTags: 'PENDING', normalTags: 'TODO', includePattern: '**/*.java', excludePattern: '**/target/**')
            ]
        }
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

def mvn(String goals){
    withMaven(
        jdk: 'JDK 1.8',
        maven: 'default', 
        mavenSettingsConfig: '05894f91-85e1-4e6d-8eb5-a101d90c62e3',
        options: [junitPublisher(disabled: true), jacocoPublisher(disabled: true), openTasksPublisher(disabled: true)]
    ) {
        sh "mvn -U $goals"
    }    
}

