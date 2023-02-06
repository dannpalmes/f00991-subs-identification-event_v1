//OKD portion
def okdname = "core-f00188-sim-card-registration"
def okdnamespace = "core-f00188-sim-card-registration"
def okdbuildconfig = "f00188-sim-card-registration-ch-barring-cqr-and-retry"
def okdisname = "f00188-sim-card-registration-ch-barring-cqr-and-retry"
//Jenkins Agent portion
def jenkinsagentname = "custom-maven-slave"
//Githook portion -- post-receive parameters from curl -d, --data <data>   HTTP POST data
def gittag = "${params.tag}"
def githashcommit = "${params.commit}"
//AWS ECR IPM Naming/Label tags for costing tracking in AWS Budget or Cost Explorer or Billing Dashboard
def ipmenvecrtagname = "ecr-docker"
def ipmprojecrtagname = "STEPS-AIM"
//AWS ECR Region where STS lies to Cross Account
def ecrawsregion = "ap-southeast-1"
//AWS ECR conventions
def organizationname = "edo-aim-dev-docker"
def awsecruri = "623551324805.dkr.ecr.ap-southeast-1.amazonaws.com"
//Skopeo auth file base on Secrets found in CICD Namespace -- auth-file-v2
def skopeoauthfile = "/myauth/auth.json"
def okddockerreg = "docker-registry.default.svc:5000"
def completeecrrepo = organizationname + "/" + okdnamespace + "/" + okdbuildconfig

//ECR complete path: docker://${awsecruri}/${organizationname}/${okdnamespace}/${okdbuildconfig}:${gittag}
//OKD IS complete path: docker://${okddockerreg}/${okdnamespace}/${okdisname}:latest

pipeline {
	options {
		disableConcurrentBuilds()
		retry(3)
		timeout(time: 1, unit: 'HOURS')
	}
	
	agent {
		node {
			label "${jenkinsagentname}"
		}
	}

	stages {
		stage('build') {
			options {
				timeout(time: 1, unit: 'HOURS')   // timeout on this stage
			}

			steps {
				sh """
					oc patch buildconfig ${okdbuildconfig} -n ${okdnamespace} \
						-p '{
								"spec":
								{
									"source": {
										"git":  {
											"ref": \"${gittag}\"
										}
								}
							}
						}'
				"""

				sh """
					oc policy add-role-to-user admin system:serviceaccount:cicd:jenkins --namespace=${okdname}
				"""

				script {
					openshift.withCluster() {
						openshift.withProject("${okdnamespace}") {
							timeout(60) {
						    	def bc= openshift.selector("bc/${okdbuildconfig}")
							    bc.startBuild("--wait")
							    bc.logs("-f")
						    }
						}
					}
				}
			}
		}//stage build

		stage('test') {
			// Here goes the TDD / JUnits / etc.. --
			options {
				timeout(time: 1, unit: 'HOURS')   // timeout on this stage
			}

			steps {
				echo 'Stage test'
			}
		}//stage test

		stage('deploy') {
			// This takes place on deployment of images to AWS ECR
			options {
				timeout(time: 1, unit: 'HOURS')   // timeout on this stage
			}

			steps {
				sh """
					completerepo=${completeecrrepo}
					awsregion=${ecrawsregion}
					status="aws ecr describe-repositories --region \$awsregion --profile prodaccess --repository-names \$completerepo" 
					echo \$status
					echo "---------------------###"
					if [[ -z `\$status` ]]; then
					    echo "Not existing aws ecr repository -- \$completerepo"

						aws ecr create-repository --profile prodaccess --repository-name ${completeecrrepo}  \
						 --region ${ecrawsregion} \
						 --tags Key=Project,Value=${ipmprojecrtagname} Key=Environment,Value=${ipmenvecrtagname} Key=Name,Value=${okdnamespace}/${okdbuildconfig} \
						 --image-tag-mutability IMMUTABLE
					else
						echo "Existing aws ecr repository, pushing new tag -- \$completerepo:${gittag}"
					fi

					export AWS_CONFIG_FILE=/home/jenkins/.aws/config && export AWS_PROFILE=prodaccess && skopeo --insecure-policy copy --src-tls-verify=false --authfile ${skopeoauthfile} \
					--dest-tls-verify=false \
					docker://${okddockerreg}/${okdnamespace}/${okdisname}:latest \
					docker://${awsecruri}/${completeecrrepo}:${gittag}
				"""
			}
		}//stage upload
	}//stages
}