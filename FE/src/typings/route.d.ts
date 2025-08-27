declare namespace AppRoute {
  type MenuType = "dir" | "page";
  interface RouteMeta {
    title: string;
    icon?: string;
    requiresAuth?: boolean;
    roles?: Entity.RoleType[];
    keepAlive?: boolean;
    hide?: boolean;
    order?: number;
    href?: string;
    activeMenu?: string;
    withoutTab?: boolean;
    pinTab?: boolean;
    menuType?: MenuType;
  }

  type MetaKeys = keyof RouteMeta;

  interface baseRoute {
    name: string;
    path: string;
    redirect?: string;
    componentPath?: string | null;
    id: number;
    pid: number | null;
  }

  type RowRoute = RouteMeta & baseRoute;

  interface Route extends baseRoute {
    children?: Route[];
    component: any;
    meta: RouteMeta;
  }
}
