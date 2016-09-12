* Update ip address in `prometheus.yml`.

* Build the docker container

```
docker build -t prometheus-demo -f Dockerfile.prometheus .
```

* Run

```
docker run -d --name prometheus-demo -p 9090:9090 prometheus-demo
```
