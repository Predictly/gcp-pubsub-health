# gcp-pubsub-health

Simple application that shows the issues with Spring Cloud GCP PubSub health check that under heavy load hangs all calls to the `/actuator/health` endpoint which combined with Kubernetes resource constraints and liveness probes causes application restarts since the liveness times out

## How to run
Modify _src/main/application.yaml_ and input a valid GCP project id. Then copy a service account key with PubSub Editor privileges to the file _src/main/jib/key.json_. Finally build and deploy the application using `skaffold run`.

Note that it is hard to replicate the crash loop scenarios using the local emulator, therefore it has to run against the public PubSub API.

### Symptoms
The pod will be restarted by Kubernetes since it fails the liveness probes, this can be experienced by looking at 

```
kubectl describe pod gcp-pubsub-health
...
Events:
  Type     Reason     Age                From               Message
  ----     ------     ----               ----               -------
  Normal   Scheduled  61s                default-scheduler  Successfully assigned default/gcp-pubsub-health to minikube
  Normal   Pulled     60s                kubelet, minikube  Container image "gcp-pubsub-health:23577444a9c0a0f156efd5d560a0cdfc45d32c47812e8302f19b4edb2fae69b3" already present on machine
  Normal   Killing    55s                kubelet, minikube  Container gcp-pubsub-health definition changed, will be restarted
  Normal   Created    42s (x2 over 60s)  kubelet, minikube  Created container gcp-pubsub-health
  Normal   Started    42s (x2 over 60s)  kubelet, minikube  Started container gcp-pubsub-health
  Normal   Pulled     42s                kubelet, minikube  Container image "gcp-pubsub-health:8a3ab0b8a34cbbe896a5ee33194da53b6054153d8728a6c89675bfe368ce821e" already present on machine
  Warning  Unhealthy  6s                 kubelet, minikube  Readiness probe failed: Get http://172.17.0.4:8080/actuator/health: net/http: request canceled (Client.Timeout exceeded while awaiting headers)
  Warning  Unhealthy  2s                 kubelet, minikube  Liveness probe failed: Get http://172.17.0.4:8080/actuator/health: net/http: request canceled (Client.Timeout exceeded while awaiting headers)
```

### Load generation
For load generation purposes, this application generates and sorts strings for each message received from PubSub.

