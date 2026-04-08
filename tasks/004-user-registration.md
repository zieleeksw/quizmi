# Task 004 - User registration

## Title
implement user registration flow

## Goal
Create the first real user flow by allowing a new user to register in the application.

## Why
This is the first business feature that writes real domain data to the database and builds the foundation for future login.

## Scope
- [ ] create user domain model
- [ ] add user persistence
- [ ] implement password hashing
- [ ] add register request validation
- [ ] expose `POST /auth/register`
- [ ] add frontend register page
- [ ] connect frontend form with backend endpoint

## Steps
- [ ] create user entity
- [ ] create user repository
- [ ] create register use case
- [ ] validate email and password
- [ ] hash password before save
- [ ] add register controller endpoint
- [ ] add register page in Angular
- [ ] handle validation and API errors in UI

## Decisions
- registration will be implemented before login
- password must never be stored in plain text
- this task should stay limited to user creation, without JWT and session handling

## Data
2026-04-08
