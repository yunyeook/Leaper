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
                withCredentials([
                    string(credentialsId: 'app-server', variable: 'APP_SERVER')
                ]) {
                    script {
                        // 환경별로 다른 EC2 서버 사용
                        def targetServer = env.APP_SERVER
                        def infraComposeFile = 'docker-compose.infra.yml'
                        def serverComposeFile = 'docker-compose.server.yml'

                        sshagent(['ec2-ssh-key']) {
                            sh """
                                # EC2 서버에 SSH 접속하여 배포
                                ssh -o StrictHostKeyChecking=no ${targetServer} '
                                    cd ./app

                                    echo "🔍 현재 실행 중인 컨테이너 확인..."
                                    docker-compose -f ${infraComposeFile} -f ${serverComposeFile} ps

                                    # 1. 기존 서버 컨테이너만 중지 및 제거 (infra는 유지)
                                    echo "🛑 기존 서버 컨테이너를 중지합니다..."
                                    docker-compose -f ${serverComposeFile} down

                                    # 2. 최신 서버 이미지 Pull 및 기존 이미지 정리
                                    echo "📥 최신 서버 이미지를 다운로드합니다..."
                                    docker rmi ${DOCKER_IMAGE}:${DOCKER_TAG} || true
                                    docker rmi ${DOCKER_IMAGE}:latest || true
                                    docker pull ${DOCKER_IMAGE}:${DOCKER_TAG}
                                    docker pull ${DOCKER_IMAGE}:latest

                                    # 3. 인프라 서비스가 실행 중인지 확인 (DB, Redis 등)
                                    echo "🔍 인프라 서비스 상태 확인..."
                                    if ! docker-compose -f ${infraComposeFile} ps | grep -q "Up"; then
                                        echo "🚀 인프라 서비스를 시작합니다..."
                                        docker-compose -f ${infraComposeFile} up -d
                                        echo "⏳ 인프라 서비스가 준비될 때까지 대기..."
                                        sleep 10
                                    else
                                        echo "✅ 인프라 서비스가 이미 실행 중입니다."
                                    fi

                                    # 4. 새로운 서버 컨테이너 시작
                                    echo "🚀 새로운 서버 컨테이너를 시작합니다..."
                                    docker-compose -f ${serverComposeFile} up -d

                                    # 5. 전체 서비스 상태 확인
                                    echo "📊 전체 서비스 상태 확인..."
                                    echo "=== 인프라 서비스 ==="
                                    docker-compose -f ${infraComposeFile} ps
                                    echo "=== 서버 서비스 ==="
                                    docker-compose -f ${serverComposeFile} ps

                                    # 6. 서버 로그 확인
                                    echo "📋 서버 로그 확인 (최근 50줄)..."
                                    docker-compose -f ${serverComposeFile} logs --tail=50 leaper-backend

                                    # 7. 오래된 이미지 정리
                                    echo "🧹 사용하지 않는 이미지 정리..."
                                    docker image prune -f
                                '
                            """
                        }
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                withCredentials([
                    string(credentialsId: 'app-server', variable: 'APP_SERVER')
                ]) {
                    script {
                        // 환경별로 다른 EC2 서버 사용
                        def targetServer = env.APP_SERVER
                        def infraComposeFile = 'docker-compose.infra.yml'
                        def serverComposeFile = 'docker-compose.server.yml'

                        // 애플리케이션이 완전히 시작될 때까지 대기
                        echo "⏳ 서버가 완전히 시작될 때까지 30초 대기..."
                        sleep(30)

                        sshagent(['ec2-ssh-key']) {
                            sh """
                                # 헬스체크 수행
                                ssh -o StrictHostKeyChecking=no ${targetServer} '
                                    cd ./app

                                    echo "🔍 서비스 헬스체크를 수행합니다..."

                                    # 인프라 서비스 상태 확인
                                    echo "=== 인프라 서비스 상태 ==="
                                    INFRA_STATUS=\$(docker-compose -f ${infraComposeFile} ps --services --filter "status=running" | wc -l)
                                    INFRA_TOTAL=\$(docker-compose -f ${infraComposeFile} config --services | wc -l)
                                    echo "인프라 서비스: \$INFRA_STATUS/\$INFRA_TOTAL 실행 중"

                                    # 서버 서비스 상태 확인
                                    echo "=== 서버 서비스 상태 ==="
                                    if docker-compose -f ${serverComposeFile} ps | grep leaper-backend | grep -q "Up"; then
                                        echo "✅ 서버 컨테이너가 정상적으로 실행 중입니다."

                                        # 추가 헬스체크: 서버 응답 확인 (포트 체크)
                                        if docker-compose -f ${serverComposeFile} exec -T leaper-backend wget --spider -q http://localhost:8080/actuator/health 2>/dev/null; then
                                            echo "✅ 서버 헬스체크 엔드포인트 응답 정상"
                                        else
                                            echo "⚠️ 헬스체크 엔드포인트 응답 없음 (아직 시작 중일 수 있음)"
                                        fi
                                    else
                                        echo "❌ 서버 컨테이너 실행 실패!"
                                        echo "=== 서버 컨테이너 로그 ==="
                                        docker-compose -f ${serverComposeFile} logs --tail=100 leaper-backend
                                        exit 1
                                    fi

                                    # 전체 서비스 요약
                                    echo "=== 배포 완료 요약 ==="
                                    echo "📊 인프라 서비스: \$INFRA_STATUS/\$INFRA_TOTAL 정상"
                                    echo "🚀 서버 서비스: 정상 실행"
                                    echo "🎉 배포가 성공적으로 완료되었습니다!"
                                '
                            """
                        }
                    }
                }
            }
        }

        stage('Cleanup') {
            steps {
                dir('backend-spring/leaper') {
                    sh '''
                        echo "🧹 로컬 Docker 이미지 정리..."
                        # 현재 빌드를 제외한 이전 버전들만 정리
                        docker images ${DOCKER_IMAGE} | grep -v latest | grep -v ${DOCKER_TAG} | awk 'NR>1 {print $1":"$2}' | xargs -r docker rmi || echo "정리할 이미지가 없습니다."
                    '''
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