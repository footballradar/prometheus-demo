1. Update ip address in `prometheus.yml`.

2. Build the docker container

```
docker build -t prometheus-demo -f Dockerfile.prometheus .
```

3. Run

```
docker run -d --name prometheus-demo -p 9090:9090 prometheus-demo
```
