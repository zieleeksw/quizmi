# Task 004 - User registration

## Title
implement user registration flow

## Goal
Create the first real user flow by allowing a new user to register in the application.

## Why
This is the first business feature that writes real domain data to the database and builds the foundation for future login.

## Scope
- [x] create user domain model
- [x] add user persistence
- [x] implement password hashing
- [x] add register request validation
- [x] expose `POST /auth/register`
- [x] create database schema with Liquibase
- [ ] add frontend register page
- [ ] connect frontend form with backend endpoint

## Steps
- [x] create user entity
- [x] create user repository
- [x] create register use case
- [x] validate email and password
- [x] hash password before save
- [x] add register controller endpoint
- [x] add Liquibase migration for `users`
- [ ] add register page in Angular
- [ ] handle validation and API errors in UI

## Decisions
- registration will be implemented before login
- password must never be stored in plain text
- this task should stay limited to user creation, without JWT and session handling

## Data
2026-04-08
