export interface UserDto {
  id: number;
  email: string;
  role: string;
}

export interface JwtDto {
  value: string;
}

export interface AuthenticationDto {
  user: UserDto;
  accessToken: JwtDto;
  refreshToken: JwtDto;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

export interface AuthSession {
  user: UserDto;
  accessToken: string;
  refreshToken: string;
}
