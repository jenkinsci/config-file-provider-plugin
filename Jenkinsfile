 node ('windows'){
  stage 'Build and Test'
  env.PATH = "${tool 'mvn'}/bin:${env.PATH}"
  checkout scm
  sh 'mvn clean package'
 }