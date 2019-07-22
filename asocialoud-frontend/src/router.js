import Vue from 'vue'
import Router from 'vue-router'

import Register from './components/Register'
import Login from './components/Login'
import Welcome from './components/Welcome'
//import App from './App'
import MemberArea from './components/MemberArea'

Vue.use(Router);

export default new Router({
    mode: "history",
    routes: [
        { path: '/', component: Welcome},
        { path: '/register', component: Register},
        { path: '/login', component: Login },
        { path: '/feed', component: MemberArea,
            meta: {
                requiresAuth: true
            }
        },

        // otherwise redirect to home
        { path: '*', redirect: '/' }
    ]
});


/*routes.beforeEach((to, from, next) => {
    if (to.matched.some(record => record.meta.requiresAuth)) {
        // this route requires auth, check if logged in
        // if not, redirect to login page.
        if (!this.$store.getters.isLoggedIn) {
            next({
                path: '/login'
            })
        } else {
            next();
        }
    } else {
        next(); // make sure to always call next()!
    }
});
*/
