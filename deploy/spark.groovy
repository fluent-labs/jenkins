pipeline {
  agent {
    kubernetes {
      yaml '''
        apiVersion: v1
        kind: Pod
        metadata:
          namespace: jobs
        spec:
          imagePullSecrets:
          - 'dockerhub'
          volumes:
          - name: sbt-cache
            persistentVolumeClaim:
              claimName: sbt-cache
          containers:
          - name: sbt
            image: lkjaero/jenkins-runners:sbt
            command:
            - sleep
            args:
            - 99d
            volumeMounts:
            - name: sbt-cache
              mountPath: /root/.cache/coursier/v1/
          - name: s3
            image: lkjaero/jenkins-runners:s3
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
        container('sbt') {
          git branch: 'master', url: 'https://github.com/fluent-labs/jobs.git'
          sh 'sbt assembly'
        }
      }
    }
    stage("Deploy the jar") {
      steps {
        container('s3') {
          sh "s3cmd --host 'fra1.digitaloceanspaces.com' --host-bucket '%(bucket)s.fra1.digitaloceanspaces.com' put target/scala-2.13/jobs.jar s3://definitions/jobs.jar"
        }
      }
    }
  }
}