**Тестовое задание.**

Напишите приложение, которое по REST принимает запрос вида POST api/v1/wallet

{
valletId: UUID,
operationType: DEPOSIT or WITHDRAW,
amount: 1000
}

после выполнять логику по изменению счета в базе данных, также есть возможность получить баланс кошелька
GET api/v1/wallets/{WALLET_UUID}

стек:

java 8-17

Spring 3

Postgresql

Должны быть написаны миграции для базы данных с помощью liquibase.

Обратите особое внимание проблемам при работе в конкурентной среде (1000 RPS поодному кошельку). Ни один запрос не должен быть не обработан (50Х error).

Предусмотрите соблюдение формата ответа для заведомо неверных запросов, когда кошелька не существует, не валидный json, или недостаточно средств.

Приложение должно запускаться в докер контейнере, база данных тоже, вся система должна подниматься с помощью docker-compose.

Предусмотрите возможность настраивать различные параметры как на стороне приложения так и базы данных без пересборки контейнеров.

Эндпоинты должны быть покрыты тестами.




**РЕШЕНИЕ**
# Wallet API

Wallet API - это RESTful сервис для управления кошельками, поддерживающий операции пополнения и снятия средств.

## Стек технологий

- Java 17
- Spring Boot 3
- PostgreSQL
- Liquibase
- Docker, Docker Compose

## Сборка и запуск

### Шаги для запуска проекта

1. **Склонируйте репозиторий:**

    ```bash
    git clone https://github.com/Alexiisssss/WalletAPI-REST.git
    cd wallet-api
    ```

2. **Соберите проект с помощью Maven:**

    ```bash
    mvn clean package
    ```

3. **Запустите Docker Compose:**

    ```bash
    docker-compose up --build
    ```

4. **Сервис будет доступен по адресу:**

    ```
    http://localhost:8080
    ```

### Примеры запросов

  - **POST** `/api/v1/wallet`
  
    ```json
    {
      "walletId": "UUID",
      "operationType": "DEPOSIT",
      "amount": 1000
    }
    ```

  - **GET**  /api/v1/wallet/{walletId} - получение баланса текущего кошелька
  
  - **POST** `/api/v1/wallet`

  ```json
  {
    "walletId": "UUID",
    "operationType": "WITHDRAW",
    "amount": 1000
  }
  ```
## **Тестирование**
   Чтобы выполнить тестирование выполните команду: mvn test

## **Миграции базы данных**
   Миграции базы данных управляются с помощью Liquibase. Файлы миграции находятся в src/main/resources/db/changelog.

## **Конкурентность**
   Приложение спроектировано для обработки конкурентных запросов к одному и тому же кошельку. Все операции обновления баланса обрабатываются атомарно и синхронизированы для предотвращения состояний гонки.

## **Настройки**
   Все параметры приложения и базы данных можно настроить через файл application.yml и переменные
