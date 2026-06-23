/** 用户资料（后端返回） */
export interface UserProfile {
  id: number;
  username: string;
  nickname: string;
  avatarUrl?: string;
  phone: string;
  email?: string;
}

/** 登录接口返回（data 字段） */
export interface LoginResult {
  token: string;
  user: UserProfile;
}
