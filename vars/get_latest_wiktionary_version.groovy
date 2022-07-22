def call(String dictionary) {
  def rss_url = "https://dumps.wikimedia.org/$dictionary/latest/$dictionary-latest-pages-meta-current.xml.bz2-rss.xml"
  def filename = "$dictionary-latest-pages-meta-current.xml.bz2-rss.xml"

  sh "wget $rss_url"
  def xml = sh script: "cat $filename", returnStdout: true

  def parsed = new XmlSlurper().parseText(xml)
  def link = parsed.channel.link.text()
  
  return (link =~ /.*\/([0-9]+)/)[ 0 ][ 1 ].toString().trim()
}