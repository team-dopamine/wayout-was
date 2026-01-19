# 프로젝트 구조
- 본 프로젝트는 TDD(Test Driven Development)로 개발을 진행한다.
- 프로젝트 패키지 구조는 아래와 같다.
  ```angular2html
  src
  ├── main/java/com/example/project
  │   ├── domain
  │   │   ├── user
  │   │   │   ├── User.java
  │   │   │   ├── UserRepository.java
  │   │   │   ├── UserService.java
  │   │   │   └── UserController.java
  │   │   ├── problem
  │   │   ├── submission
  │   │   ├── generator
  │   │   ├── validator
  │   │   └── testcase
  │   ├── global
  │   │   ├── config
  │   │   ├── exception
  │   │   └── common
  │   └── infra
  │       └── docker
  │
  └── test/java/com/example/project
      └── domain
          ├── user
          │   ├── UserServiceTest.java
          │   └── UserRepositoryTest.java
          ├── problem
          └── ...
  ```
