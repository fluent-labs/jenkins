pipeline {
  agent {
    kubernetes {
      yaml '''
        apiVersion: v1
        kind: Pod
        metadata:
          namespace: content
        spec:
          imagePullSecrets:
          - 'dockerhub'
          containers:
          - name: spark
            image: lkjaero/jenkins-runners:spark
            imagePullPolicy: Always
            command:
            - sleep
            args:
            - 99d
            env:
            - name: AWS_ACCESS_KEY_ID
              valueFrom:
                secretKeyRef:
                  name: do-spaces-access-key
                  key: username
            - name: AWS_SECRET_ACCESS_KEY
              valueFrom:
                secretKeyRef:
                  name: do-spaces-access-key
                  key: password
      '''
    }
  }
  
  stages {
    stage("Build the jar") {
      steps {
        container('spark') {
          git branch: 'develop', url: 'https://github.com/fluent-labs/jobs.git'
          sh 'sbt assembly'
        }
      }
    }
    stage("Deploy the jar") {
      steps {
        container('spark') {
          sh "s3cmd --host 'fra1.digitaloceanspaces.com' --host-bucket '%(bucket)s.fra1.digitaloceanspaces.com' put target/scala-2.13/jobs.jar s3://definitions/jobs.jar"
        }
      }
    }
  }
}