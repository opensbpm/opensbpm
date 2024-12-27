
'''
kubectl --kubeconfig ~/.kube/opensbm\@hetzner -k . apply
'''

== only needed fro local development
'''
kubectl --kubeconfig ~/.kube/opensbm\@hetzner create -n opensbpm secret generic engine-db-password --from-literal=password=OpenSBPM
kubectl --kubeconfig ~/.kube/opensbm\@hetzner create -n opensbpm secret generic keycloak-db-password --from-literal=password=OpenSBPM
'''


== Grafana access with port-forwarding
(see https://www.server-world.info/en/note?os=Ubuntu_24.04&p=microk8s&f=8 )
'''
microk8s kubectl port-forward -n observability service/kube-prom-stack-grafana --address 0.0.0.0 3000:80 
'''
