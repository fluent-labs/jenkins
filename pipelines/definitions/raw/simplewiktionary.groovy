def dictionary = 'simplewiktionary'
def latest_wiktionary_version = null
def latest_downloaded_version = null

pipeline {
  agent {
    kubernetes {
      yamlFile 'pipelines/definitions/raw/raw_pod.yaml'
    }
  }
  stages {
    stage("Check download") {
      steps {
        container('s3') {
          script {
            latest_wiktionary_version = get_latest_wiktionary_version(dictionary)
            latest_downloaded_version = get_latest_processed_version(dictionary)

            echo "Latest $dictionary version: $latest_wiktionary_version"
            echo "Latest processed $dictionary version: $latest_downloaded_version"
          }
        }
      }
    }
    stage("Download latest") {
      when { not { equals expected: latest_wiktionary_version, actual: latest_downloaded_version } }
      steps {
        container('s3') {
          script {
            echo "Fetching newer $dictionary version $latest_wiktionary_version."
            def dump_file = get_wiktionary_dump(dictionary, latest_wiktionary_version)
            
            sh "s3cmd --host 'fra1.digitaloceanspaces.com' --host-bucket '%(bucket)s.fra1.digitaloceanspaces.com' put $dump_file s3://definitions/$dictionary/$latest_wiktionary_version/raw/$dump_file"
        
            put_latest_processed_version(dictionary, latest_wiktionary_version)
          }
        }
      }
    }
  }
}