FROM gradle:7.6.3-jdk17
COPY --chown=gradle:gradle . .
WORKDIR .
RUN gradle build -x test --no-daemon
EXPOSE 8081
CMD ["gradle", "bootRun"]