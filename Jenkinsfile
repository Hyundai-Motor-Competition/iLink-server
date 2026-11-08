// main push → 테스트 → 이미지(Harbor ilink/api:{sha}) → dev 배포(ilink-dev). 롤아웃이 실패하면 직전 버전으로 되돌린다
// Credentials: ilink-harbor(로봇 계정 robot$ilink+jenkins-ci), ilink-kubeconfig(ilink-deployer 토큰)
pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    triggers {
        githubPush()
    }

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-21-openjdk'
        PATH = "/usr/local/bin:${env.PATH}"   // k3s kubectl 위치. jenkins 사용자 PATH에는 없다
        REGISTRY = 'harbor.chisanw.com'
        IMAGE = 'harbor.chisanw.com/ilink/api'
        NAMESPACE = 'ilink-dev'
    }

    stages {
        stage('Test') {
            steps {
                sh './gradlew test --no-daemon --console=plain'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                }
            }
        }

        stage('Image') {
            steps {
                script {
                    env.TAG = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
                }
                sh './gradlew bootJar --no-daemon --console=plain'
                withCredentials([usernamePassword(credentialsId: 'ilink-harbor',
                        usernameVariable: 'HARBOR_USER', passwordVariable: 'HARBOR_PASSWORD')]) {
                    sh '''
                        echo "$HARBOR_PASSWORD" | docker login "$REGISTRY" -u "$HARBOR_USER" --password-stdin
                        docker build --pull -t "$IMAGE:$TAG" .
                        docker push "$IMAGE:$TAG"
                    '''
                }
            }
            post {
                always {
                    sh 'docker logout "$REGISTRY" || true; docker image rm "$IMAGE:$TAG" || true'
                }
            }
        }

        stage('Deploy dev') {
            steps {
                withCredentials([file(credentialsId: 'ilink-kubeconfig', variable: 'KUBECONFIG')]) {
                    sh '''
                        CONFIG_HASH=$(sed -n '/^kind: ConfigMap/,/^---/p' ops/k8s/dev/api.yaml | sha256sum | cut -c1-12)
                        sed -e "s|__IMAGE__|$IMAGE:$TAG|" -e "s|__CONFIG_HASH__|$CONFIG_HASH|" ops/k8s/dev/api.yaml \
                            | kubectl apply -f -
                        if ! kubectl -n "$NAMESPACE" rollout status deploy/api --timeout=300s; then
                            kubectl -n "$NAMESPACE" rollout undo deploy/api
                            exit 1
                        fi
                    '''
                }
            }
        }
    }
}
