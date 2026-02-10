/// <reference path="../global.d.ts"/>

namespace Entity {
    interface UserInformation {
    userId: string | undefined
    userCode: string | undefined
    fullName: string | undefined
    phone: string | undefined
    rolesNames: string[] | string
    rolesCodes: string[] | string
    email: string | undefined
    pictureUrl: string | undefined
    roleSwitch: string | undefined
    roleScreen: string | undefined // role Lấy từ Fe khi login
    }
}
