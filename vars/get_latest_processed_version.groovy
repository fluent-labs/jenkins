def call(String dictionary) {
  try {
    sh "s3cmd --host 'fra1.digitaloceanspaces.com' --host-bucket '%(bucket)s.fra1.digitaloceanspaces.com' get s3://definitions/$dictionary/latest_version.txt"
    def latest_version = sh script: "cat latest_version.txt", returnStdout: true
    sh "rm latest_version.txt"
    return latest_version.toString().trim()
  } catch (Exception e) {
    echo "Couldn't get latest processed version. Is this the first time you're running this?"
    echo 'Exception occurred: ' + e.toString()
    return null
  }
}