
'''
kubectl --kubeconfig ~/.kube/opensbm\@hetzner -k . apply
'''

== only needed for local development
'''
kubectl --kubeconfig ~/.kube/opensbm\@hetzner create -n opensbpm secret generic engine-db-password --from-literal=password=OpenSBPM
kubectl --kubeconfig ~/.kube/opensbm\@hetzner create -n opensbpm secret generic keycloak-db-password --from-literal=password=OpenSBPM
'''

=== fetch tls secret
'''
kubectl --kubeconfig ~/.kube/opensbm\@hetzner get secret -n opensbpm opensbpm-tls -o yaml > k8s/secret-tls.yaml
'''

=== ingress logs
'''
kubectl --kubeconfig ~/.kube/opensbm\@hetzner logs -n ingress -l name=nginx-ingress-microk8s
'''

== Dashboard access with port-forwarding

(see https://microk8s.io/docs/addon-dashboard )
'''
microk8s kubectl port-forward -n kube-system service/kubernetes-dashboard --address 0.0.0.0 10443:443
'''


== Grafana access with port-forwarding
(see https://www.server-world.info/en/note?os=Ubuntu_24.04&p=microk8s&f=8 )
'''
microk8s kubectl port-forward -n observability service/kube-prom-stack-grafana --address 0.0.0.0 3000:80 
'''
