# Stage 1: Build
FROM python:3.9-slim AS builder
WORKDIR /app
COPY pyproject.toml setup.cfg* ./
RUN pip install --no-cache-dir --prefix=/install .
COPY . .
RUN pip install --no-cache-dir --prefix=/install .

# Stage 2: Runtime
FROM python:3.9-slim
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /install /usr/local
COPY . .
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8080/health')" || exit 1
ENTRYPOINT ["python", "-m", "my-cli-tool"]
