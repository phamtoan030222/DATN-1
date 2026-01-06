/// <reference path="../global.d.ts"/>


namespace Api {
  namespace Login {
    interface Info {
      accessToken: string;
      refreshToken: string;
      userInfo: Entity.UserInformation;
    }
  }
}
