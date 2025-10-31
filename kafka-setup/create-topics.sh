#!/bin/bash

# BROKER=103.166.183.142:9092
BROKER=localhost:9092
REPLICATION_FACTOR=1
PARTITIONS=1

create_topic() {
  docker exec -i kafka-1 bash -c "kafka-topics --bootstrap-server $BROKER --create --if-not-exists --replication-factor $REPLICATION_FACTOR --partitions $PARTITIONS --topic $1"

}

echo "Creating Kafka topics..."

TOPICS=(
  auth.login.request
  auth.token.verified
  auth.logout.request

  task.created
  task.updated
  task.deleted
  task.assigned
  task.deadline.alert

  notification.send.email
  notification.send.zalo
  notification.send.system
  notification.status.failed

  chat.message.sent
  chat.message.received
  chat.user.typing

  user.created
  user.updated
  user.deleted
  user.status.changed

  class.created
  class.updated
  class.member.added
  class.member.removed

  schedule.created
  schedule.reminder.triggered
  schedule.updated

  score.submitted
  score.reviewed
  score.updated

  document.uploaded
  document.updated
  document.deleted
  document.shared

  search.index.request
  search.indexed.response
  search.failure

  document.index.request
  user.index.request
  task.index.request

  github.repo.connected
  github.webhook.received
  github.commit.synced

  google.drive.synced
  google.calendar.updated
  google.sheet.data.pushed
)

for topic in "${TOPICS[@]}"; do
  echo "Creating topic: $topic"
  create_topic "$topic"
done

echo "✅ Done creating topics."
