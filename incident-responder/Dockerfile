FROM python:3.12-alpine

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    AUDIT_LOG_PATH=/var/log/responder/audit.jsonl

WORKDIR /app
RUN addgroup -S responder && adduser -S -G responder responder \
    && mkdir -p /var/log/responder \
    && chown -R responder:responder /app /var/log/responder

COPY --chown=responder:responder app.py /app/app.py

USER responder
EXPOSE 8080
CMD ["python", "-u", "app.py"]
