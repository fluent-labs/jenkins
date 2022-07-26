def dictionary = 'simplewiktionary'
def mainClassName = 'SimpleWiktionarySectionFinder'

pipeline {
  agent {
    kubernetes {
      yamlFile 'definitions/analyze/analyze_pod.yaml'
    }
  }
//   triggers {
//     cron('H H(11-15) * * *')
//   }
  stages {
    stage("Check download") {
      steps {
        container('kubernetes') {
          script {
            latest_downloaded_version = get_latest_processed_version(dictionary)
            echo "Latest processed $dictionary version: $latest_downloaded_version"

            withKubeConfig([credentialsId: 'jenkins-operator-token', namespace: 'content']) {
                // image: gcr.io/spark-operator/spark:v3.1.1
                    sh """
cat <<EOF | kubectl apply -f -
apiVersion: sparkoperator.k8s.io/v1beta2
kind: SparkApplication
metadata:
    name: analyze-sections-$dictionary
    namespace: content
spec:
    type: Scala
    mode: cluster
    
    mainClass: io.fluentlabs.jobs.definitions.analyze.wiktionary.section.$mainClassName
    mainApplicationFile: s3://definitions/jobs.jar
    sparkVersion: "3.3.0"
    arguments: 
    - s3://definitions/
    - $latest_downloaded_version
    driver:
        javaOptions: "-Dlog4j.configurationFile=/mnt/config/log4j2.xml"
        cores: 1
        coreLimit: "1200m"
        memory: "512m"
        labels:
            version: 3.3.0
        serviceAccount: spark-spark
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
    executor:
        javaOptions: "-Dlog4j.configurationFile=log4j2.xml"
        cores: 1
        instances: 1
        memory: "512m"
        labels:
            version: 3.3.0
        serviceAccount: spark-spark
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
EOF
                    """
                }
          }
        }
      }
    }
  }
}