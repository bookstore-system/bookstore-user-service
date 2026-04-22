pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'truongdocker1'
        DOCKER_CREDENTIALS_ID = 'dockerhub-creds'
        
        DB_CREDS = credentials('db-creds')
        APP_JWT_SECRET = credentials('user-service-jwt-secret')
        
        IMAGE_NAME = 'bookstore-user-service'

        // version nên linh hoạt theo build number (chuẩn CI/CD)
        TAG = "${BUILD_NUMBER}"

        K8S_DEPLOYMENT = 'user-service-deployment'
        K8S_CONTAINER = 'user-service'
    }

    tools {
        maven 'Maven 3.9'
        jdk 'JDK 21'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    dockerImage = docker.build(
                        "${DOCKER_REGISTRY}/${IMAGE_NAME}:${TAG}",
                        "."
                    )
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry(
                        'https://index.docker.io/v1/',
                        "${DOCKER_CREDENTIALS_ID}"
                    ) {
                        dockerImage.push()
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh """
                export KUBECONFIG=/var/jenkins_home/.kube/config

                # update image tag
                sed -i "s|image: truongdocker1/bookstore-user-service:latest|image: ${DOCKER_REGISTRY}/${IMAGE_NAME}:${TAG}|g" k8s/deployment.yaml

                # 1. ConfigMap OK (có trong Git)
                kubectl apply -f k8s/configmap.yaml

                # 2. Secret tạo runtime (KHÔNG cần file)
                kubectl create secret generic user-service-secret \
                --from-literal=DB_USERNAME=$DB_CREDS_USR \
                --from-literal=DB_PASSWORD=$DB_CREDS_PSW \
                --from-literal=APP_JWT_SECRET=$APP_JWT_SECRET \
                --dry-run=client -o yaml | kubectl apply -f -

                # 3. Deploy app
                kubectl apply -f k8s/deployment.yaml
                kubectl apply -f k8s/service.yaml

                kubectl rollout status deployment/${K8S_DEPLOYMENT}
                """
            }
        }
    }

    post {
        success {
            echo "Build & Deploy SUCCESS"
        }
        failure {
            echo "Build FAILED"
        }
    }
}
