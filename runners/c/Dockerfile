FROM gcc:14.4.0-trixie

RUN install -d -m 0700 -o 10001 -g 10001 /work

ENV HOME=/work
WORKDIR /work
USER 10001:10001

CMD ["gcc", "--version"]
