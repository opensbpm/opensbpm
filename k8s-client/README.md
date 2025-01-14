
Run in Google Cloud

Create ~application-dev.yaml~ with
```
```
e2e:
    statistics:
        url: <webdavurl>
        username: <username>
        password: <password>
```
kubectl --kubeconfig "%USERPROFILE%/.kube/opensbpm-client@gcloud.yaml" create configmap e2e-client --from-file=application.yaml=engine\e2e\src\main\resources\application-dev.yaml
```

CMD:
```
kubectl --kubeconfig "%USERPROFILE%/.kube/opensbpm-client@gcloud.yaml" -f k8s-client/job.yaml apply
```
