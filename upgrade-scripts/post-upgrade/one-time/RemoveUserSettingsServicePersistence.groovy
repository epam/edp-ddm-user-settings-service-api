void call() {
    sh "helm uninstall user-settings-service-persistence -n ${NAMESPACE} || true;"
}

return this;
