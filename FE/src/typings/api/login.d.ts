/// <reference path="../global.d.ts"/>

namespace Api {
  namespace Login {
    interface Info extends Entity.User {
      id: number;
      role: Entity.RoleType[];
      accessToken: string;
      refreshToken: string;
    }
  }
}
