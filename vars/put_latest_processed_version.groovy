def call(String dictionary, String latest_version) {
  sh """
  echo $latest_version > latest_processed_version.txt
  s3cmd --host 'fra1.digitaloceanspaces.com' --host-bucket '%(bucket)s.fra1.digitaloceanspaces.com' put latest_processed_version.txt s3://definitions/$dictionary/latest_version.txt
  """
}