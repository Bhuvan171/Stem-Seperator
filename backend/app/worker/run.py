from redis import Redis
from rq import Queue
from rq.worker import SimpleWorker

from app.config import settings

QUEUE_NAME = "separation"


def main():
    # SimpleWorker (not the default forking Worker): forking per job would break the
    # CUDA context and defeat keeping the model warm in VRAM on the GPU server. Using
    # it locally too so the same worker config works unchanged when SEPARATOR=real.
    conn = Redis.from_url(settings.redis_url)
    queue = Queue(QUEUE_NAME, connection=conn)
    SimpleWorker([queue], connection=conn).work()


if __name__ == "__main__":
    main()
