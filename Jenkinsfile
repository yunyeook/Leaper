pipeline {
    agent {
        docker {
            image 'docker:24.0.7-dind'
            args '--privileged -v /var/run/docker.sock:/var/run/docker.sock --user root'
        }
    }

    environment {
        DOCKER_IMAGE = 'shinjwde/leaper-backend'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Install Java and Setup') {
            steps {
                sh '''
                    # Alpine Linux에서 Java 17 설치
                    apk add --no-cache openjdk17 curl bash openssh-client git

                    # 환경 변수 설정
                    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
                    echo "JAVA_HOME=$JAVA_HOME" >> /etc/environment

                    echo "=== 환경 정보 확인 ==="
                    java -version
                    docker --version
                    git --version
                '''
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Setup Environment') {
            steps {
                script {
                    def envFile = env.BRANCH_NAME == 'main' ? '.env' : '.env'

                    withCredentials([file(credentialsId: envFile, variable: 'ENV_FILE')]) {
                        sh '''
                            cd backend-spring/leaper
                            cp $ENV_FILE .env.dev
                            echo ".env 파일이 설정되었습니다"
                            ls -la .env.dev
                        '''
                    }
                }
            }
        }

        stage('Build Spring Boot') {
            steps {
                dir('backend-spring/leaper') {
                    sh '''
                        # Gradle wrapper에 실행 권한 부여
                        chmod +x ./gradlew

                        # 스프링 부트 빌드 (테스트 포함)
                        ./gradlew clean build -x test

                        # 빌드된 JAR 파일 확인
                        ls -la build/libs/
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                dir('backend-spring/leaper') {
                    script {
                        // Docker 이미지 빌드
                        def image = docker.build("${DOCKER_IMAGE}:${DOCKER_TAG}", "--no-cache .")
                        // latest 태그도 함께 생성
                        sh "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"

                        echo "Docker 이미지 빌드 완료: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    }
                }
            }
        }

        stage('Push to Registry') {
            steps {
                script {
                    // Docker Hub에 푸시
                    docker.withRegistry('', 'docker-hub-credentials') {
                        docker.image("${DOCKER_IMAGE}:${DOCKER_TAG}").push()
                        docker.image("${DOCKER_IMAGE}:latest").push()
                    }
                }
            }
        }

        stage('Deploy to EC2') {
            steps {
                script {
                    // 환경별로 다른 EC2 서버 사용
                    def targetServer = 'ubuntu@ip-172-26-12-81'
                    def composeFile = env.BRANCH_NAME == 'main' ? 'docker-compose.prod.yml' : 'docker-compose.dev.yml'

                    sshagent(['ec2-ssh-key']) {
                        sh """
                            # EC2 서버에 SSH 접속하여 배포
                            ssh -o StrictHostKeyChecking=no ${targetServer} '
                                # 현재 실행 중인 컨테이너 중지
                                cd ./app
                                docker-compose -f ${composeFile} down

                                # 최신 이미지 Pull
                                docker rmi ${DOCKER_IMAGE}:${DOCKER_TAG} || true
                                docker rmi ${DOCKER_IMAGE}:latest || true
                                docker pull ${DOCKER_IMAGE}:${DOCKER_TAG}
                                docker pull ${DOCKER_IMAGE}:latest

                                # 오래된 이미지 정리
                                docker image prune -f

                                # 새로운 컨테이너 시작
                                docker-compose -f ${composeFile} up -d

                                # 컨테이너 상태 확인
                                docker-compose -f ${composeFile} ps

                                # 로그 확인 (최근 50줄)
                                docker-compose -f ${composeFile} logs --tail=50 leaper-backend
                            '
                        """
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    def targetServer = 'ubuntu@ip-172-26-12-81'

                    // 애플리케이션이 완전히 시작될 때까지 대기
                    sleep(30)

                    sshagent(['ec2-ssh-key']) {
                        sh """
                            # 헬스체크 수행
                            ssh -o StrictHostKeyChecking=no ${targetServer} '
                                # 컨테이너 상태 확인
                                if docker-compose -f ./app/docker-compose.${env.BRANCH_NAME == 'main' ? 'prod' : 'dev'}.yml ps | grep -q "Up"; then
                                    echo "✅ 컨테이너가 정상적으로 실행 중입니다."

                                else
                                    echo "❌ 컨테이너 실행 실패!"
                                    exit 1
                                fi
                            '
                        """
                    }
                }
            }
        }

        stage('Cleanup') {
            steps {
                dir('backend-spring/leaper') {
                    sh '''
                        # 로컬 Docker 이미지 정리
                        docker images shinjwde/leaper-backend | grep -v latest | awk 'NR>1 {print $1":"$2}' | xargs docker rmi                    '''
                }
            }
        }
    }

    post {
        always {
            // 워크스페이스 정리
            cleanWs()
        }
        success {
            echo "✅ Docker 이미지 빌드가 성공적으로 완료되었습니다!"
            echo "🐳 이미지: ${DOCKER_IMAGE}:${DOCKER_TAG}"
        }
        failure {
            echo "❌ 빌드가 실패했습니다. 로그를 확인해주세요."
        }
    }
}