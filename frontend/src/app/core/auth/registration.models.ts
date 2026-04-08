export interface RegisterRequest {
  email: string;
  password: string;
}

export interface UserDto {
  id: number;
  email: string;
  role: string;
}
