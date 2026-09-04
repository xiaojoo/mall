pipeline {
  agent {
    kubernetes {
      yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: maven
    image: swr.cn-southwest-2.myhuaweicloud.com/ks/kubesphere/builder-maven:v3.2.0-podman
    command:
    - cat
    tty: true
    volumeMounts:
    - name: workspace-volume
      mountPath: /home/jenkins/agent
    - name: maven-repo-volume
      mountPath: /root/.m2/repository
  - name: kaniko
    image: crpi-jnk3ch80my2cet7k.cn-shenzhen.personal.cr.aliyuncs.com/mall-devops/kaniko:1.1
    command:
    - cat
    tty: true
    volumeMounts:
    - name: workspace-volume
      mountPath: /home/jenkins/agent
  volumes:
  - name: workspace-volume
    emptyDir: {}
  - name: maven-repo-volume
    persistentVolumeClaim:
      claimName: maven-repo-pvc
"""
    }
  }
  environment {
    DOCKER_CREDENTIAL_ID = 'aliyun-id'
    GITEE_CREDENTIAL_ID = 'gitee-id'
    KUBECONFIG_CREDENTIAL_ID = 'demo-kubeconfig'
    REGISTRY = 'crpi-jnk3ch80my2cet7k.cn-shenzhen.personal.cr.aliyuncs.com'
    DOCKERHUB_NAMESPACE = 'mall-devops'
    GITEE_ACCOUNT = 'sunxiaojie350'
    BRANCH_NAME = 'master'
  }
  parameters {
    string(name: 'PROJECT_VERSION', defaultValue: '0.00001')
    // string(name: 'PROJECT_NAME', defaultValue: 'mall-gateway', description: '构建模块')
    choice(name: 'PROJECT_NAME', choices: ['mall-gateway', 'mall-auth', 'mall-cart', 'mall-coupon',
    'mall-fast', 'mall-member', 'mall-order', 'mall-product', 'mall-search', 'mall-product',
    'mall-search', 'mall-seckill','mall-third-party', 'mall-ware'])
  }
  stages {
    stage('拉取代码') {
      steps {
        git(url: 'https://gitee.com/sunxiaojie/mall.git', credentialsId: 'gitee-id', branch: 'master', changelog: true, poll: false)
        script {
          echo "正在构建 ${params.PROJECT_NAME} 版本号：${params.PROJECT_VERSION} 将会提交给 ${env.REGISTRY} 镜像仓库"
        }
        container('maven') {
          sh 'ls -la /root/.m2/repository'
          sh 'du -sh /root/.m2/repository'  // 查看目录大小
          sh 'mvn clean install -Dmaven.test.skip=true -gs `pwd`/mvn-settings.xml'
        }
      }
    }
    stage('构建镜像') {
      steps {
        container('maven') {
          sh 'mvn -Dmaven.test.skip=true -gs `pwd`/mvn-settings.xml clean package'
        }
      }
    }
    stage('推送镜像') {
      steps {
        container('kaniko') {
          sh "ls"
          sh "cd ${params.PROJECT_NAME} && ls"
          withCredentials([usernamePassword(credentialsId: 'aliyun-id', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
            sh """
              cd ${params.PROJECT_NAME} && kaniko -f Dockerfile -c ./ -d \$REGISTRY/\$DOCKERHUB_NAMESPACE/${params.PROJECT_NAME}:SNAPSHOT-\$BRANCH_NAME-\$BUILD_NUMBER --force
            """
          }
        }
      }
    }
    stage('部署到k8s') {
      steps {
        container('maven') {
          input(id: "deploy-to-dev-${params.PROJECT_NAME}", message: "是否将 ${params.PROJECT_NAME} 部署到集群中?")
          withCredentials([kubeconfigContent(credentialsId: 'demo-kubeconfig', variable: 'KUBECONFIG_CONFIG')]) {
            sh 'mkdir -p ~/.kube/'
            sh 'echo "$KUBECONFIG_CONFIG" > ~/.kube/config'
            sh 'ls'
            sh "cd ${params.PROJECT_NAME} && ls"
            sh "cd ${params.PROJECT_NAME} && envsubst < deploy/${params.PROJECT_NAME}-deploy.yaml | kubectl apply -f -"
          }
        }
      }
    }
  }
}