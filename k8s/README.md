
'''
kubectl --kubeconfig ~/.kube/opensbm\@hetzner -k . apply
'''

== only needed fro local development
'''
kubectl --kubeconfig ~/.kube/opensbm\@hetzner create -n opensbpm secret generic engine-db-password --from-literal=password=OpenSBPM
kubectl --kubeconfig ~/.kube/opensbm\@hetzner create -n opensbpm secret generic keycloak-db-password --from-literal=password=OpenSBPM
'''

