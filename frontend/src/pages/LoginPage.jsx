import LoginForm from "../components/LoginForm";
import Navbar from "../components/Navbar";

function LoginPage() {
    return (
        <>
            <Navbar />
            <main className="container d-flex flex-grow-1 align-items-center justify-content-center py-5">
                <div className="col-12 col-sm-8 col-md-6 col-lg-5">
                    <LoginForm />
                </div>
            </main>
        </>
    );
}

export default LoginPage;
