import { jwtDecode } from 'jwt-decode'

export function getUserInformation(token: string): Entity.UserInformation {
  const decoded = jwtDecode<Entity.DecodedToken>(token)

  return {
    userId: decoded.userId,
    userCode: decoded.userCode,
    fullName: decoded.fullName,
    rolesNames: decoded.rolesName,
    rolesCodes: decoded.rolesCode,
    email: decoded.email,
    pictureUrl: decoded.pictureUrl,
    roleSwitch: decoded.roleSwitch,
    roleScreen: decoded.roleScreen,
  }
}

export function getRolesUser(token: string): string[] {
  const decoded = jwtDecode<Entity.DecodedToken>(token)
  return decoded.rolesCode
}

export function getExpireTime(token: string): number {
  const decoded = jwtDecode<Entity.DecodedToken>(token)
  return decoded.exp
}
