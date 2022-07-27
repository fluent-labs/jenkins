def get_hash_for_version(dictionary, version) {
  sh "wget https://dumps.wikimedia.org/$dictionary/$version/dumpstatus.json"
  def json = sh script: "cat dumpstatus.json", returnStdout: true

  def hash = sh script "echo $json | jq '.jobs.metacurrentdump.files.$dictionary-$version-pages-meta-current.xml.bz2.sha1'", returnStdout: true
  return hash.trim()
}

def call(String dictionary, String version) {
  def dump_url = "http://download.wikimedia.org/$dictionary/$version/$dictionary-$version-pages-meta-current.xml.bz2"
  def archive_filename = "$dictionary-$version-pages-meta-current.xml.bz2"
  
  // Download the archive
  sh "wget $dump_url"
  
  // Check the hash
  def expected_hash = get_hash_for_version(dictionary, version)
  def actual_hash = sh script: "sha1sum $archive_filename", returnStdout: true
  actual_hash = (actual_hash =~ /([^ ]+) .*/)[ 0 ][ 1 ].toString().trim()
  echo "Expected: $expected_hash, actual: $actual_hash"
  if (actual_hash != expected_hash) {
    throw new IllegalStateException("Download hash did not match, expected: $expected_hash, actual: $actual_hash")
  }
  
  // Unzip it
  sh "bzip2 -d $archive_filename"

  return "$dictionary-$version-pages-meta-current.xml"
}