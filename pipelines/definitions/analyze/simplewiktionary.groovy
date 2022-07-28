def dictionary = 'simplewiktionary'
def mainClassName = 'SimpleWiktionarySectionFinder'
def latest_downloaded_version = null

pipeline {
  agent {
    kubernetes {
      yamlFile 'pipelines/definitions/analyze/analyze_pod.yaml'
    }
  }
  stages {
    stage("Check download") {
      steps {
        container('s3') {
          script {
            latest_downloaded_version = get_latest_processed_version(dictionary)
            echo "Latest processed $dictionary version: $latest_downloaded_version"
          }
        }
      }
    }
    stage("Trigger job") {
      steps {
        container('kubernetes') {
          script {
            withKubeConfig([credentialsId: 'jenkins-operator-token', namespace: 'content']) {
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
    image: lkjaero/spark-runner:3.2.2
    imagePullSecrets:
    - dockerhub
    mainClass: io.fluentlabs.jobs.definitions.analyze.wiktionary.section.$mainClassName
    mainApplicationFile: s3a://definitions/jobs.jar
    sparkVersion: "3.2.2"
    arguments: 
    - s3a://definitions/
    - "$latest_downloaded_version"
    driver:
        javaOptions: "-Dlog4j.configurationFile=/mnt/config/log4j2.xml"
        cores: 1
        coreLimit: "1200m"
        memory: "512m"
        labels:
            version: 3.2.2
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
            version: 3.2.2
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