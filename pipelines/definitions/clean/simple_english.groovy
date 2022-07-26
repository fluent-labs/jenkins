podTemplate(
    containers: [
        containerTemplate(name: 'kubernetes', image: 'alpine/k8s:1.21.13', command: 'sleep', args: '99d'),
        containerTemplate(name: 'sbt', image: 'lkjaero/spark-runner:latest', command: 'sleep', args: '99d')
    ],
    activeDeadlineSeconds: 3600,
    imagePullSecrets: ['dockerhub'],
    serviceAccount: 'jenkins-spark-job'
) {
    node(POD_LABEL) {
        container('kubernetes') {
            stage('Submit job to Spark') {
                withKubeConfig([credentialsId: 'jenkins-operator-token', namespace: 'content']) {
                    sh '''
cat <<EOF | kubectl apply -f -
apiVersion: sparkoperator.k8s.io/v1beta2
kind: SparkApplication
metadata:
    name: simple-english
    namespace: content
spec:
    type: Scala
    mode: cluster
    image: lkjaero/spark-runner:latest
    mainClass: com.foreignlanguagereader.jobs.definitions.SimpleWiktionary
    mainApplicationFile: local:///home/jenkins/agent/target/scala-2.13/jobs.jar
    sparkVersion: "3.0.0"
    driver:
        javaOptions: "-Dlog4j.configurationFile=/mnt/config/log4j2.xml"
        cores: 1
        coreLimit: "1200m"
        memory: "512m"
        labels:
            version: 3.0.0
        serviceAccount: spark-spark
    executor:
        javaOptions: "-Dlog4j.configurationFile=log4j2.xml"
        cores: 1
        instances: 1
        memory: "512m"
        labels:
            version: 3.0.0
        serviceAccount: spark-spark
EOF
                    '''
                }
            }
        }
    }
}